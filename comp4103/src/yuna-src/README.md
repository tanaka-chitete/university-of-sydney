# Yuna

Yuna is a fully self-contained web-based computational notebook solution written in Python, JavaScript, and HTML with the ultimate goal of lowering the barriers to entry for using computational notebooks.

## Getting started
1. Clone the `yuna` repository into the desired directory:
```
git clone https://github.com/tanaka-chitete/yuna.git
```
2. Open a terminal window at the target directory.
3. Change into the `/yuna` directory:
```
cd yuna
```

## Getting to work
### Creating an environment
1. Change into the `/src` directory:
```
cd src
```
2. Create an environment:
```
python3 yuna.py <environment_filename> [requirements_filename]
```
Where:
- `envrionment_filename` is the name of the new environment, including the `.html` extension.
- `requirements_filename` is the name of the optional requirements file, including the `.txt` extension. The requirements file must be a newline-delimited list:
```
pandas
kiwisolver
gmpy2
```

</br>

*Yuna will copy the* `template.html` *environment, embed the requirements, and save the new environment to the* `/out` *directory.*


*See* [**List of supported packages**](#list-of-supported-packages) *for the list of supported packages.*

### Readying the environment
Open the new environment file in the desired browser.

*Yuna will load the embedded requirements and deserialise the embedded objects.*

### Writing in the notebook
1. Click the various operations listed under the "Edit" menu to use operations that allow for adding code (Python) and text (Markdown) cells. 
2. In addition to the action buttons located to the left of each cell, click the various operations listed under the "Run" menu to use operations that allow for running code and previewing text.

### Saving the environment
Click the "Save environment" operation listed under the "File" menu.

*Yuna will save all code and text, embed all instantiated objects, and download the saved environment to the browser's specified downloads directory.*

## Appendix
### [List of supported packages](#list-of-supported-packages)
- `asciitree`
- `astropy`
- `atomicwrites`
- `attrs`
- `autograd`
- `bcrypt`
- `beautifulsoup4`
- `biopython`
- `bitarray`
- `bleach`
- `bokeh`
- `boost-histogram`
- `brotli`
- `cbor-diag`
- `certifi`
- `cftime`
- `click`
- `cligj`
- `cloudpickle`
- `cmyt`
- `colorspacious`
- `coverage`
- `cramjam`
- `cryptography`
- `cssselect`
- `cycler`
- `cytoolz`
- `decorator`
- `demes`
- `distlib`
- `docutils`
- `exceptiongroup`
- `fastparquet`
- `fiona`
- `fonttools`
- `fsspec`
- `future`
- `galpy`
- `gensim`
- `geopandas`
- `gmpy2`
- `gsw`
- `h5py`
- `html5lib`
- `idna`
- `imageio`
- `iniconfig`
- `jedi`
- `Jinja2`
- `joblib`
- `jsonschema`
- `kiwisolver`
- `lazy-object-proxy`
- `lightgbm`
- `logbook`
- `lxml`
- `MarkupSafe`
- `matplotlib`
- `matplotlib-pyodide`
- `mne`
- `more-itertools`
- `mpmath`
- `msgpack`
- `msprime`
- `multidict`
- `munch`
- `mypy`
- `networkx`
- `newick`
- `nlopt`
- `nltk`
- `nose`
- `numcodecs`
- `numpy`
- `opencv-python`
- `optlang`
- `pandas`
- `parso`
- `patsy`
- `Pillow`
- `pkgconfig`
- `pluggy`
- `py`
- `pyb2d`
- `pyclipper`
- `pycparser`
- `pycryptodome`
- `pydantic`
- `pyerfa`
- `Pygments`
- `pyheif`
- `pyinstrument`
- `pynacl`
- `pyodide-http`
- `pyparsing`
- `pyproj`
- `pyrsistent`
- `pytest`
- `pytest-benchmark`
- `python-dateutil`
- `python-magic`
- `python-sat`
- `pytz`
- `pywavelets`
- `pyxel`
- `pyyaml`
- `rebound`
- `reboundx`
- `regex`
- `retrying`
- `RobotRaconteur`
- `scikit-image`
- `scikit-learn`
- `scipy`
- `setuptools`
- `shapely`
- `six`
- `soupsieve`
- `sparseqr`
- `sqlalchemy`
- `statsmodels`
- `svgwrite`
- `swiglpk`
- `sympy`
- `tblib`
- `termcolor`
- `threadpoolctl`
- `tomli`
- `tomli-w`
- `toolz`
- `tqdm`
- `traits`
- `tskit`
- `typing-extensions`
- `uncertainties`
- `unyt`
- `webencodings`
- `wordcloud`
- `wrapt`
- `xarray`
- `xgboost`
- `xlrd`
- `yarl`
- `yt`
- `zarr`