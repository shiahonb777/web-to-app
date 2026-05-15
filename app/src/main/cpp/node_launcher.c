#include <dlfcn.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>

typedef int (*node_start_func)(int argc, char** argv);

static const char* WTA_NODE_LIB = "WTA_NODE_LIB";

int main(int argc, char* argv[]) {
    const char* node_lib_path = getenv(WTA_NODE_LIB);
    if (node_lib_path == NULL || node_lib_path[0] == '\0') {
        fprintf(stderr, "%s is not set\n", WTA_NODE_LIB);
        return 111;
    }

    struct stat st;
    if (stat(node_lib_path, &st) != 0) {
        fprintf(stderr, "Node runtime not found: %s\n", node_lib_path);
        return 112;
    }

    signal(SIGPIPE, SIG_IGN);

    void* handle = dlopen(node_lib_path, RTLD_NOW | RTLD_GLOBAL);
    if (handle == NULL) {
        fprintf(stderr, "dlopen failed: %s\n", dlerror());
        return 113;
    }

    void* symbol = dlsym(handle, "node_start");
    if (symbol == NULL) {
        symbol = dlsym(handle, "_ZN4node5StartEiPPc");
    }
    if (symbol == NULL) {
        fprintf(stderr, "node::Start symbol not found: %s\n", dlerror());
        return 114;
    }

    return ((node_start_func) symbol)(argc, argv);
}
