# LT_Code
A quick (and suboptimal) way to generate redundent data to tackle with erasure using Luby transform code

# Usage
Construct Encoder with the data and size of each frame, then call next() as many times as your heart desires. 
Decoder will be initialized with the first frame received, or it can be initialized manually. Call frameReceived() when a new frame is received. 

# Overhead
From my rudimentary testing, the overhead is large but is proportionally smaller for very large arrays (can be down to 10% in some cases, or up to 100% in one test). There are methods to give current decoding status. 

# Credit
The project was inspired by another project on GitHub https://github.com/k13n/soliton_distribution by k13n
