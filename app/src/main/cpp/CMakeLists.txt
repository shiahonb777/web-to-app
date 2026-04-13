# CMakeLists.txt for WebToApp Crypto Engine

cmake_minimum_required(VERSION 3.22.1)

project("crypto_engine")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

function(enable_16kb_page_support target_name)
    # 强制共享库按 16 KB ELF 页面大小对齐，避免 Android 15+ 进入兼容模式。
    target_link_options(${target_name} PRIVATE "-Wl,-z,max-page-size=16384")
endfunction()

# 添加编译选项（crypto_engine 专用）
# 注意：不使用全局 add_compile_options，因为 node_bridge 需要导出 JNI 符号

# 源文件
set(CRYPTO_SOURCES
    crypto_engine.cpp
    anti_debug.cpp
    integrity_check.cpp
    aes_gcm.cpp
)

# 创建共享库
add_library(
    crypto_engine
    SHARED
    ${CRYPTO_SOURCES}
)
enable_16kb_page_support(crypto_engine)

# crypto_engine 编译选项：隐藏符号、优化
target_compile_options(crypto_engine PRIVATE
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -O2
    -DNDEBUG
)

# ==================== Node.js Bridge ====================
# JNI 桥接层：通过 dlopen 加载 libnode.so 并调用 node::Start()
add_library(
    node_bridge
    SHARED
    node_bridge.cpp
)
enable_16kb_page_support(node_bridge)

# 查找 Android 日志库
find_library(
    log-lib
    log
)

# 链接库
target_link_libraries(
    crypto_engine
    ${log-lib}
)

# crypto_engine 可以 strip 所有符号
set_target_properties(crypto_engine PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections -Wl,--strip-all"
)

# ==================== Optimized Crypto (优化版加密引擎) ====================
# 修复: 恒定时间 S-Box, 标准 PBKDF2-HMAC-SHA256 (RFC 2898),
#       HKDF (RFC 5869), HMAC-SHA256 (RFC 2104), 4x CTR 并行
add_library(
    crypto_optimized
    SHARED
    crypto_optimized.c
)
enable_16kb_page_support(crypto_optimized)

set_property(TARGET crypto_optimized PROPERTY C_STANDARD 11)

target_compile_options(crypto_optimized PRIVATE
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -O2
    -DNDEBUG
    -Wall
    -Wextra
)

target_link_libraries(
    crypto_optimized
    ${log-lib}
)

set_target_properties(crypto_optimized PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections -Wl,--strip-all"
)


target_link_libraries(
    node_bridge
    ${log-lib}
)

# node_bridge 编译选项：保留 JNI 导出符号（不使用 -fvisibility=hidden）
target_compile_options(node_bridge PRIVATE
    -ffunction-sections
    -fdata-sections
    -O2
    -DNDEBUG
)

# node_bridge 只 gc-sections，不 strip（保留 JNI 导出符号）
set_target_properties(node_bridge PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections"
)

# ==================== Hardware Control (黑科技底层控制) ====================
# JNI 原生硬件控制层：通过 sysfs / ioctl / input 子系统绕过 Android API
add_library(
    hardware_control
    SHARED
    hardware_control.c
)
enable_16kb_page_support(hardware_control)

# 使用 C11 标准
set_property(TARGET hardware_control PROPERTY C_STANDARD 11)

# 编译选项：保留 JNI 导出符号，不隐藏
target_compile_options(hardware_control PRIVATE
    -ffunction-sections
    -fdata-sections
    -O2
    -DNDEBUG
    -Wall
    -Wextra
)

# 链接 log (Android 日志) + m (数学库: sin/cos/sqrt)
target_link_libraries(
    hardware_control
    ${log-lib}
    m
)

# 只 gc-sections，保留 JNI 导出符号
set_target_properties(hardware_control PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections"
)

# ==================== APK Optimizer (APK 体积优化引擎) ====================
# C 级 ZIP 重压缩 + 条目裁剪，用于极致压缩生成的 APK
add_library(
    apk_optimizer
    SHARED
    apk_optimizer.c
)
enable_16kb_page_support(apk_optimizer)

# 使用 C11 标准
set_property(TARGET apk_optimizer PROPERTY C_STANDARD 11)

# 查找 zlib (Android NDK 自带)
find_library(z-lib z)

# 编译选项：体积优化 (-Os) + 隐藏符号 + LTO
target_compile_options(apk_optimizer PRIVATE
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -Os
    -DNDEBUG
    -Wall
    -Wextra
    -flto
)

# 链接 zlib (DEFLATE 重压缩) + log (Android 日志)
target_link_libraries(
    apk_optimizer
    ${log-lib}
    ${z-lib}
)

# LTO + gc-sections + strip (优化器自身也要小)
set_target_properties(apk_optimizer PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections -Wl,--strip-all -flto"
)

# ==================== Performance Engine (极致性能优化引擎) ====================
# C 级热路径加速：URL 解析、MIME 查找、mmap I/O、性能 JS 注入
add_library(
    perf_engine
    SHARED
    perf_engine.c
)
enable_16kb_page_support(perf_engine)

set_property(TARGET perf_engine PROPERTY C_STANDARD 11)

# -O3 最大速度优化 (性能引擎不需要体积优化)
target_compile_options(perf_engine PRIVATE
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -O3
    -DNDEBUG
    -Wall
    -Wextra
)

target_link_libraries(
    perf_engine
    ${log-lib}
)

set_target_properties(perf_engine PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections -Wl,--strip-all"
)

# ==================== System Optimizer (系统层极致性能优化) ====================
# C 级系统调优：CPU 亲和性、线程优先级、OOM 评分、I/O 预读、温度监控
add_library(
    sys_optimizer
    SHARED
    sys_optimizer.c
)
enable_16kb_page_support(sys_optimizer)

set_property(TARGET sys_optimizer PROPERTY C_STANDARD 11)

# -O3 最大速度优化
target_compile_options(sys_optimizer PRIVATE
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -O3
    -DNDEBUG
    -Wall
    -Wextra
)

target_link_libraries(
    sys_optimizer
    ${log-lib}
)

set_target_properties(sys_optimizer PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections -Wl,--strip-all"
)

# ==================== Browser Kernel (浏览器内核伪装引擎) ====================
# 反 WebView 检测: navigator/chrome/WebGL/Permissions/Error stack 全面伪装
add_library(
    browser_kernel
    SHARED
    browser_kernel.c
)
enable_16kb_page_support(browser_kernel)

set_property(TARGET browser_kernel PROPERTY C_STANDARD 11)

# -Os 体积优化 (此库主要是静态字符串, 不需要 -O3 速度优化)
target_compile_options(browser_kernel PRIVATE
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -Os
    -DNDEBUG
    -Wall
    -Wextra
)

target_link_libraries(
    browser_kernel
    ${log-lib}
)

set_target_properties(browser_kernel PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections -Wl,--strip-all"
)
