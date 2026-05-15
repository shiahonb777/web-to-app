#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h>
#include <unistd.h>

#ifndef EI_CLASS
#define EI_CLASS 4
#endif

#ifndef ELFCLASS32
#define ELFCLASS32 1
#endif

#ifndef ELFCLASS64
#define ELFCLASS64 2
#endif

#ifndef AT_EMPTY_PATH
#define AT_EMPTY_PATH 0x1000
#endif

#ifndef MFD_CLOEXEC
#define MFD_CLOEXEC 0x0001U
#endif

#define LINKER32_PATH "/system/bin/linker"
#define LINKER64_PATH "/system/bin/linker64"

extern char** environ;

static int create_memfd(const char* name) {
#ifdef SYS_memfd_create
    return (int) syscall(SYS_memfd_create, name, MFD_CLOEXEC);
#elif defined(__NR_memfd_create)
    return (int) syscall(__NR_memfd_create, name, MFD_CLOEXEC);
#else
    errno = ENOSYS;
    return -1;
#endif
}

static int execveat_empty_path(int fd, char* const argv[]) {
#ifdef SYS_execveat
    return (int) syscall(SYS_execveat, fd, "", argv, environ, AT_EMPTY_PATH);
#elif defined(__NR_execveat)
    return (int) syscall(__NR_execveat, fd, "", argv, environ, AT_EMPTY_PATH);
#else
    errno = ENOSYS;
    return -1;
#endif
}

static int copy_fd(int in_fd, int out_fd) {
    char buffer[64 * 1024];
    while (1) {
        ssize_t read_count = read(in_fd, buffer, sizeof(buffer));
        if (read_count == 0) return 0;
        if (read_count < 0) {
            if (errno == EINTR) continue;
            return -1;
        }

        ssize_t written = 0;
        while (written < read_count) {
            ssize_t write_count = write(out_fd, buffer + written, (size_t) (read_count - written));
            if (write_count < 0) {
                if (errno == EINTR) continue;
                return -1;
            }
            written += write_count;
        }
    }
}

static const char* choose_linker_path(int source_fd) {
    unsigned char ident[5];
    if (lseek(source_fd, 0, SEEK_SET) < 0) {
        return NULL;
    }

    ssize_t read_count = read(source_fd, ident, sizeof(ident));
    if (read_count < (ssize_t) sizeof(ident)) {
        return NULL;
    }

    if (ident[0] != 0x7f || ident[1] != 'E' || ident[2] != 'L' || ident[3] != 'F') {
        return NULL;
    }

    if (ident[EI_CLASS] == ELFCLASS64) {
        return LINKER64_PATH;
    }
    if (ident[EI_CLASS] == ELFCLASS32) {
        return LINKER32_PATH;
    }
    return NULL;
}

int main(int argc, char* argv[]) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <executable> [args...]\n", argv[0]);
        return 64;
    }

    char* target_path = argv[1];

    execv(target_path, &argv[1]);
    if (errno != EACCES && errno != EPERM) {
        fprintf(stderr, "execv failed for %s: %s\n", target_path, strerror(errno));
        return 111;
    }

    int source_fd = open(target_path, O_RDONLY | O_CLOEXEC);
    if (source_fd < 0) {
        fprintf(stderr, "open failed for %s: %s\n", target_path, strerror(errno));
        return 112;
    }

    /*
     * Android's bionic linker is invoked as:
     *   /system/bin/linker64 <absolute-program-path> [args...]
     * and resolves dependent libraries via LD_LIBRARY_PATH.
     *
     * That lets us execute readable ELF binaries from app-private storage even
     * when execve(target_path, ...) is blocked by the filesystem mount policy.
     */
    const char* linker_path = choose_linker_path(source_fd);
    if (linker_path != NULL) {
        execve(linker_path, argv, environ);
        fprintf(stderr, "linker exec failed for %s via %s: %s\n",
                target_path, linker_path, strerror(errno));
    }

    if (lseek(source_fd, 0, SEEK_SET) < 0) {
        fprintf(stderr, "lseek failed for %s: %s\n", target_path, strerror(errno));
        close(source_fd);
        return 117;
    }

    int memfd = create_memfd("wta-go-exec");
    if (memfd < 0) {
        fprintf(stderr, "memfd_create failed: %s\n", strerror(errno));
        close(source_fd);
        return 113;
    }

    if (copy_fd(source_fd, memfd) != 0) {
        fprintf(stderr, "copy to memfd failed for %s: %s\n", target_path, strerror(errno));
        close(source_fd);
        close(memfd);
        return 114;
    }

    close(source_fd);

    if (execveat_empty_path(memfd, &argv[1]) != 0) {
        fprintf(stderr, "execveat failed for %s: %s\n", target_path, strerror(errno));
        close(memfd);
        return 115;
    }

    return 116;
}
