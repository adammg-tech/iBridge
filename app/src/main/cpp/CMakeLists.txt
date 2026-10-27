cmake_minimum_required(VERSION 3.22.1)
project("ibridge_core")

add_library(
    ibridge_core
    SHARED
    ibridge_core.c
)

find_library(log-lib log)

target_link_libraries(
    ibridge_core
    ${log-lib}
)
