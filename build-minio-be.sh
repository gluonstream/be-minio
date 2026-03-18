#!/usr/bin/env bash

# Use docker buildx for cross-compilation from ARM to AMD64
docker build --platform linux/amd64 -t gluonstream/be-minio:latest .

# Push the image to the registry
docker push gluonstream/be-minio:latest

# Optional: Load the image if using a local kind cluster
# kind load docker-image gluonstream/be-minio:latest --name blog.s4v3

# Restart the deployment to pick up the new image
kubectl rollout restart deployment.apps/be-minio -n minio-namespace
