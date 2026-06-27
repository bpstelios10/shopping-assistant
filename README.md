# SHOPPING-ASSISTANT


## BUILD + RUN

`sh
# Start colima
colima start -p llm --cpu 4 --memory 8 --vm-type=vz --mount-type=virtiofs
colima start -p llm

docker-compose up --build
`
