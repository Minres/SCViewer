cmake -B build -S . -DBUILD_SHARED_LIBS=ON -DCMAKE_INSTALL_PREFIX=../../linux-x86-64
cmake --build build --target install
