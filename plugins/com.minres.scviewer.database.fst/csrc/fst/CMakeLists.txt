cmake_minimum_required (VERSION 3.0)

project (fstlib VERSION 1.0.0)

set(BUILD_SHARED_LIBS ON)
#find_package(ZLIB REQUIRED)
include(FetchContent)
FetchContent_Declare(
	zlib
    URL https://www.zlib.net/zlib-1.2.13.tar.gz
        https://www.zlib.net/fossils/zlib-1.2.13.tar.gz
        https://github.com/madler/zlib/releases/download/v1.2.13/zlib-1.2.13.tar.gz
    URL_HASH MD5=9b8aa094c4e5765dabf4da391f00d15c
)
FetchContent_Populate(zlib)
add_subdirectory(${zlib_SOURCE_DIR} ${zlib_BINARY_DIR} EXCLUDE_FROM_ALL)
add_library(ZLIB::ZLIB ALIAS zlibstatic)
target_include_directories(zlibstatic INTERFACE ${zlib_BINARY_DIR} ${zlib_SOURCE_DIR})

set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
add_library(fstapi fstapi.c lz4.c fastlz.c fst_helper.c)
#target_include_directories(fstapi PUBLIC ${CMAKE_CURRENT_SOURCE_DIR} ${ZLIB_INCLUDE_DIRS})
target_link_libraries(fstapi PRIVATE zlibstatic)
# hack to avoid creating dummy config.h
target_compile_definitions(fstapi PRIVATE -DFST_CONFIG_INCLUDE="fstapi.h")

if(MSVC)
    # define __MINGW32__ to minimize changes to upstream
    target_compile_definitions(fstapi PRIVATE __MINGW32__  _CRT_SECURE_NO_WARNINGS  FST_DO_MISALIGNED_OPS)
    target_compile_options(fstapi PRIVATE /wd4244 /wd4267 /wd4146 /wd4996)
endif()

install(TARGETS fstapi
	LIBRARY DESTINATION .
	RUNTIME DESTINATION .)
