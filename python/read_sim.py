import json
import numpy as np
import matplotlib.pyplot as plt

plt.ion()

fig, ax = plt.subplots()

def get_spectrogram_data(data, shape=(129,6144)):

    header, simdata = data.split('\n',1)

    complex_data = np.frombuffer(simdata, dtype='i1').astype(np.float32).view(np.complex64)
    #complex_data = np.fromfile(ff, dtype='i1').astype(np.float32).view(np.complex64)
    complex_data = complex_data.reshape(*shape)
    cpfft = np.fft.fftshift( np.fft.fft(complex_data), 1)
    spectrogram = np.abs(cpfft)**2
    return spectrogram

def get_spectrogram(filename, shape=(32,6144), skip_lines = 1):
    ff = open(filename,'rb')

    for i in range(0,skip_lines):
      _ = json.loads(ff.readline())

    #raw_data = ff.read()  
    #complex_data = np.frombuffer(raw_data, dtype='i1').astype(np.float32).view(np.complex64)
    complex_data = np.fromfile(ff, dtype='i1').astype(np.float32).view(np.complex64)
    complex_data = complex_data.reshape(*shape)
    cpfft = np.fft.fftshift( np.fft.fft(complex_data), 1)
    spectrogram = np.abs(cpfft)**2
    return spectrogram

def scale(spectrogram, max=255.0, min=False):
  if min:
    spectrogram = spectrogram - spectrogram.min()
  spectrogram = max * spectrogram / spectrogram.max()
  return spectrogram

def to_json(fin,fout):
  ff = open(fin,'rb')
  header = json.loads(ff.readline())
  raw_data = ff.read()
  header['raw_data'] = raw_data
  json.dump()
#isn't this faster?
#spectrogram = cpfft.real**2 + cpfft.imag**2

def read_and_show(filename='test.data', log=False, aspect=True, skip_lines=1):

  spectrogram = get_spectrogram(filename, skip_lines=skip_lines)
  if log:
    spectrogram = np.log(spectrogram)

  if(aspect):
    ax.imshow(spectrogram, 
      aspect = 0.5*float(spectrogram.shape[1]) / spectrogram.shape[0])
  else:
    ax.imshow(spectrogram)

def log_spectrum(filename='test.data'):
  spectrogram = get_spectrogram(filename)
  spectrum = np.sum(spectrogram, axis=0)
  ax.semilogy(range(0,len(spectrum)), spectrum)
