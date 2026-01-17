# CMakeLists.txt for WebToApp Crypto Engine

cmake_minimum_required(VERSION 3.22.1)

project("crypto_engine")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 添加编译选项
add_compile_options(
    -fvisibility=hidden          # 隐藏符号
    -ffunction-sections          # 函数分段
    -fdata-sections              # 数据分段
    -O2                          # 优化级别
    -DNDEBUG                     # 禁用调试
)

# 添加链接选项
add_link_options(
    -Wl,--gc-sections            # 移除未使用的段
    -Wl,--strip-all              # 移除符号表
)

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
