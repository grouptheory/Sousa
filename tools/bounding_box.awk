# Usage: image_info  <file> | awk -f bounding_box.awk
# Usage: nuiSDK_print <file> | awk -f bounding_box.awk
#
# Extracts the bounding box in decimal degrees of an image file from this
# fraction of nuiSDK_print's output:
# ...
#                    Left: 77  15' 24.04" W
#                     Top: 39  1' 55.14" N
#                   Right: 76  49' 58.09" W
#                  Bottom: 38  45' 13.47" N
# ...
# or this fraction of image_info's output:
# ...
#                  image0.lr_lat:  31.211626880661715
#                  image0.lr_lon:  34.350189720226091
# ...
#                  image0.ul_lat:  31.304771959281521
#                  image0.ul_lon:  34.220257492344373
# ...                
#
# Outputs comma-separated text in lon, lat order.
# For input from nuiSDK_print, the output is
#
# Left, Top, Right, Bottom
#
#
# For input from image_info, the output is
#
# image0.ul_lon, image0.ul_lat, image0.ll_lon, image0.ll_lat
#

BEGIN { 
	left = top = right = bottom = 0.0
}

# Left longitude
/Left:/ { # nuiSDK_print output
	left = $2 + ($3/60.0) + ($4/3600.0)
	if ($5 == "W") left *= -1.0
}

/image0.ul_lon:/ { # ossim image_info output
	left = $2
}


# Top latitude
/Top:/ { # nuiSDK_print output
	top = $2 + ($3/60.0) + ($4/3600.0)
	if ($5 == "S") top *= -1.0
}

/image0.ul_lat:/ { # ossim image_info output
	top = $2
}

# Right longitude
/Right:/ { # nuiSDK_print output 
	right = $2 + ($3/60.0) + ($4/3600.0)
	if ($5 == "W") right *= -1.0
}

/image0.lr_lon:/ { # ossim image_info output
	right = $2
}


# Bottom latitude
/Bottom:/ { # nuiSDK_print output
	bottom = $2 + ($3/60.0) + ($4/3600.0)
	if ($5 == "S") bottom *= -1.0
}

/image0.lr_lat:/ { # ossim image_info output
	bottom = $2
}

END { print left "," top "," right "," bottom}
