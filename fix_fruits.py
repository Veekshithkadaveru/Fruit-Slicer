import os
from PIL import Image, ImageOps
import cv2
import numpy as np

def fix_fruit_image(filepath):
    # If image has an alpha channel, we use it to find contours
    img = cv2.imread(filepath, cv2.IMREAD_UNCHANGED)
    if img is None:
        print(f"Skipping {filepath}, cannot read")
        return
        
    if len(img.shape) == 3 and img.shape[2] == 4:
        alpha = img[:,:,3]
        _, thresh = cv2.threshold(alpha, 10, 255, cv2.THRESH_BINARY)
    else:
        # no alpha, convert to gray and threshold
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        # Assuming white background, we want dark objects
        _, thresh = cv2.threshold(gray, 240, 255, cv2.THRESH_BINARY_INV)

    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    if not contours:
        print(f"No contours found in {filepath}")
        return

    # Sort contours by area
    contours = sorted(contours, key=cv2.contourArea, reverse=True)
    
    # Check if there are multiple large contours (like 2 fruits side-by-side)
    # We will keep just the largest one!
    main_contour = contours[0]
    
    # Create a mask for the main contour
    mask = np.zeros_like(thresh)
    cv2.drawContours(mask, [main_contour], -1, 255, -1)
    
    # Create the result image by keeping only the main contour portion
    result = img.copy()
    if len(img.shape) == 3 and img.shape[2] == 4:
        result[:,:,3] = cv2.bitwise_and(img[:,:,3], mask)
    else:
        # Set everything outside mask to white or transparent if we add alpha
        rgba = cv2.cvtColor(img, cv2.COLOR_BGR2BGRA)
        rgba[:,:,3] = mask
        result = rgba
        
    # Crop to the bounding box of the main contour
    x, y, w, h = cv2.boundingRect(main_contour)
    # Add a bit of padding
    pad = 10
    x = max(0, x - pad)
    y = max(0, y - pad)
    w = min(img.shape[1] - x, w + 2 * pad)
    h = min(img.shape[0] - y, h + 2 * pad)
    
    cropped = result[y:y+h, x:x+w]
    
    cv2.imwrite(filepath, cropped)
    print(f"Fixed {filepath}, keeping largest contour.")

fruits = [
    "app/src/main/res/drawable/fruit_strawberry.png",
    "app/src/main/res/drawable/fruit_watermelon.png",
    "app/src/main/res/drawable/fruit_orange.png",
    "app/src/main/res/drawable/fruit_grapes.png",
    "app/src/main/res/drawable/fruit_pineapple.png",
    "app/src/main/res/drawable/fruit_cherry.png",
    "app/src/main/res/drawable/fruit_lemon.png"
]

for f in fruits:
    if os.path.exists(f):
        fix_fruit_image(f)
