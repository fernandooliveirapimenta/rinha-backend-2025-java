#!/bin/bash

set -e
APP_NAME="rinha-backen-2025-java"
DOCKER_USER="fernando107"
VERSION=$(git rev-parse --short HEAD)
IMAGE_NAME="$DOCKER_USER/$APP_NAME"

echo "🐳 Build da imagem Docker..."
docker build -f Dockerfile.native -t $IMAGE_NAME:$VERSION -t $IMAGE_NAME:latest .

echo "✅ Build concluído:"
echo "  - $IMAGE_NAME:$VERSION"
echo "  - $IMAGE_NAME:latest"


echo "🔐 Enviando imagens..."
docker push $IMAGE_NAME:$VERSION
docker push $IMAGE_NAME:latest
echo "🎉 Imagens enviadas!"

