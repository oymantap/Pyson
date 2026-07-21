cmake_minimum_required(VERSION 3.22.1)
project("pyson")

add_library(
    pyson_engine
    SHARED
    pyson_engine.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    pyson_engine
    ${log-lib}
)
