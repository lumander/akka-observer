val path="/home/path/ciao/observed_20180904.csv"


path.split("/").last.matches("observed_[0-9]{8}.csv")

