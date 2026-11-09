cmake_minimum_required(VERSION 3.22.1)
project("echolink_audio" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)
find_library(log-lib log)

add_library(
    echolink_audio
    SHARED
    echolink_audio_engine.cpp
)

target_link_libraries(
    echolink_audio
    oboe::oboe
    ${log-lib}
)
