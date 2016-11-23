import json
import numpy as np

ff = open('test.data','rb')

header = json.loads(ff.readline())

raw_data = ff.read()
complex_data = np.frombuffer(raw_data, dtype='i1').astype(np.float32).view(np.complex64)

## WORK IN PROGRESS TO FFT AND CREATE SPECTROGRAM -- for some reason, initial attempt failed. 
complex_data = complex_data.reshape(129,6144)
