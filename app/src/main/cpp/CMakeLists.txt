# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name.
project("amogusapp")

# Define the paths to the libraries
set(OPENSSL32_ROOT_DIR "D:\\Libraries\\openssl_build32")
set(ZLIB32_ROOT "D:\\Libraries\\zlib_build32")
set(CURL32_ROOT "D:\\Libraries\\curl_build32")

set(OPENSSL64_ROOT_DIR "D:\\Libraries\\openssl_build64")
set(ZLIB64_ROOT "D:\\Libraries\\zlib_build64")
set(CURL64_ROOT "D:\\Libraries\\curl_build64")

# Define the architecture based on a CMake variable or environment variable
if("${ANDROID_ABI}" STREQUAL "armeabi-v7a")
        set(OPENSSL_ROOT_DIR ${OPENSSL32_ROOT_DIR})
        set(ZLIB_ROOT ${ZLIB32_ROOT})
        set(CURL_ROOT ${CURL32_ROOT})
elseif("${ANDROID_ABI}" STREQUAL "arm64-v8a")
        set(OPENSSL_ROOT_DIR ${OPENSSL64_ROOT_DIR})
        set(ZLIB_ROOT ${ZLIB64_ROOT})
        set(CURL_ROOT ${CURL64_ROOT})
else()
        message(FATAL_ERROR "Unsupported ABI: ${ANDROID_ABI}")
endif()

include_directories(
        ${OPENSSL_ROOT_DIR}/include
        ${CURL_ROOT}/include
        ${ZLIB_ROOT}/include
)

# List C/C++ source files with relative paths to this CMakeLists.txt.
#add_library(${CMAKE_PROJECT_NAME} SHARED camerax_api.cpp connections_api.cpp)
add_library(camerax_api SHARED camerax_api.cpp)
add_library(connections_api SHARED connections_api.cpp)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

set(CURL_LIBRARY ${CURL_ROOT}/lib/libcurl.so )
set(OPENSSL_LIBRARIES
        ${OPENSSL_ROOT_DIR}/lib/libssl.so
        ${OPENSSL_ROOT_DIR}/lib/libcrypto.so
)
set(ZLIB_LIBRARY ${ZLIB_ROOT}/lib/libz.so)

target_link_libraries(camerax_api
        android
        ${log-lib}
        ${jnigraphics-lib}
        ${CURL_LIBRARY}
        ${OPENSSL_LIBRARIES}
        ${ZLIB_LIBRARY}
)

target_link_libraries(connections_api
        android
        ${log-lib}
        ${jnigraphics-lib}
        ${CURL_LIBRARY}
        ${OPENSSL_LIBRARIES}
        ${ZLIB_LIBRARY}
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O3")
