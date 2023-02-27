cmake -B build -S . -DCMAKE_INSTALL_PREFIX=../../linux-x86-64 -DCMAKE_BUILD_TYPE=Release
cmake --build build --target install
