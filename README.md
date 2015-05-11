1P Project Templates
============================
Basic project templates that can be used as a starting point
for 1P projects.

Templates
----------

### 1p-service

Basic microservice.

### 1p-async-service

Asynchronous microservice with websocket and SSE (server side events) support.

### 1p-app

Client Angular.js application demonstrating how to interact with 1P microservices.

How to Use
----------

1. Clone the template into a new folder with the name of the project.

    Create `my-async-service` from the `1p-async-service` template.

    ```bash
    cd ~/projects/1p-project-templates
    git archive HEAD:1p-async-service --prefix my-async-service/ | tar -x -C ~/projects/
    cd ~/projects/my-async-service
    ```

2. Rename your properties file to the name of your project.

    ```bash
    mv src/main/resources/1p-async-service.properties src/main/resources/my-async-service.properties
    ```

3. Update files with correct project name

    ```bash
    find . -type f \( -name "*.java" -o -name "*.properties" \) -exec sed -i '' 's/1p-async-service/my-async-service/g' {} \;
    ```

4. Update Eureka service name

	```bash
	sed -i '' 's/1P_ASYNC_SERVICE_TEMPLATE/MY_ASYNC_SERVICE/g' src/main/resources/my-async-service.properties
	```

5. Build project and run tests

	```bash
	gradle clean build
	```

6. *optional* Copy .gitignore and initialize git

	```bash
	cp ~/projects/1p-project-templates/.gitignore .
	git init
	git add .
	git commit -m "Initial Import"
	```
