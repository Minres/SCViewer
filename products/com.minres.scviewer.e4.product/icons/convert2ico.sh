convert $1.png -compress none \
    \( -clone 0 -resize 16x16 -compress none \) \
    \( -clone 0 -resize 24x24 -compress none \) \
    \( -clone 0 -resize 32x32 -compress none \) \
    \( -clone 0 -resize 48x48 -compress none \) \
    \( -clone 0 -resize 16x16 -colors 256 -compress none \) \
    \( -clone 0 -resize 24x24 -colors 256 -compress none \) \
    \( -clone 0 -resize 32x32 -colors 256 -compress none \) \
    \( -clone 0 -resize 48x48 -colors 256 -compress none \) \
    \( -clone 0 -resize 256x256 -compress none \) \
    -delete 0 $1.ico
