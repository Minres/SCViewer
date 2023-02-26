cmake_minimum_required (VERSION 3.0)

project (fstlib VERSION 1.0.0)

set(BUILD_SHARED_LIBS ON)
find_package(ZLIB REQUIRED)

add_library(fstapi fstapi.c lz4.c fastlz.c)
target_include_directories(fstapi PUBLIC ${CMAKE_CURRENT_SOURCE_DIR} ${ZLIB_INCLUDE_DIRS})
target_link_libraries(fstapi PRIVATE ${ZLIB_LIBRARIES})
# hack to avoid creating dummy config.h
target_compile_definitions(fstapi PRIVATE -DFST_CONFIG_INCLUDE="fstapi.h")

if(MSVC)
    # define __MINGW32__ to minimize changes to upstream
    target_compile_definitions(fstapi PRIVATE __MINGW32__  _CRT_SECURE_NO_WARNINGS  FST_DO_MISALIGNED_OPS)
    target_compile_options(fstapi PRIVATE /wd4244 /wd4267 /wd4146 /wd4996)
endif()

install(TARGETS fstapi
	LIBRARY DESTINATION .)
