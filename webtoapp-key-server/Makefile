.PHONY: help build run test clean docker-build docker-run

help:
	@echo "WebToApp Key Server - Available commands:"
	@echo ""
	@echo "  make build              - Build the application"
	@echo "  make run                - Run the application"
	@echo "  make test               - Run tests"
	@echo "  make fmt                - Format code"
	@echo "  make lint               - Run linter"
	@echo "  make clean              - Clean build artifacts"
	@echo "  make docker-build       - Build Docker image"
	@echo "  make docker-run         - Run with Docker Compose"
	@echo "  make docker-stop        - Stop Docker containers"
	@echo "  make db-migrate         - Run database migrations"
	@echo ""

build:
	go build -o bin/keyserver ./cmd/main.go

run:
	go run ./cmd/main.go

test:
	go test -v -cover ./...

fmt:
	go fmt ./...

lint:
	golangci-lint run ./...

clean:
	rm -rf bin/
	rm -f data/keyserver.db

docker-build:
	docker build -f docker/Dockerfile -t webtoapp-keyserver:latest .

docker-run:
	docker-compose up -d

docker-stop:
	docker-compose down

docker-logs:
	docker-compose logs -f keyserver

db-migrate:
	go run ./cmd/main.go -migrate

.DEFAULT_GOAL := help
