echo "Assembling jar"
sbt assembly

echo "Jar assembled"

echo "Dockerizing!"
docker-compose up --build -d
