import PIL.Image as Image
import os
import glob

fruits = glob.glob('app/src/main/res/drawable/fruit_*.png')
for f in fruits:
    img = Image.open(f)
    print(f"{os.path.basename(f)}: size {img.size[0]}x{img.size[1]}, ratio {img.size[0]/img.size[1]:.2f}")
    
    # If width is much larger than height, crop left half
    if img.size[0] > img.size[1] * 1.3:
        print(f"  -> Cropping right side off! It's a double asset!")
        left_half = img.crop((0, 0, img.size[0] // 2, img.size[1]))
        left_half.save(f)
        print(f"  -> Done.")
