import json
import numpy as np

ff = open('test.data','rb')

header = json.loads(ff.readline())

raw_data = ff.read()
complex_data = np.frombuffer(raw_data, dtype='i1').astype(np.float32).view(np.complex64)

#reshape to waterfall shape
complex_data = complex_data.reshape(129,6144)

cpfftd = np.fft.fftshift( np.fft.fft(complext_data), 1)
spectrogram = np.abs(cpfft)**2
# isn't this the same and faster?
# spectrogram = cpfft.real**2 + cpfft.imag**2

import matplotlib.pyplot as plt
plt.ion()

fig, ax = plt.subplots()

ax.imshow(spectrogram)

ax.imshow(spectrogram, 
  aspect = 0.5*float(spectrogram.shape[1]) / spectrogram.shape[0])

## the only issue at the moment is the reversed frequency bins compared to the PIL library read of the PPM file generated with SETIKit.
