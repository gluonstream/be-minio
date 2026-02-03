# Kubernetes Deployment Instructions

This folder contains the Kubernetes manifests for deploying the `be-minio` application along with its dependencies (PostgreSQL and MinIO).

## Prerequisites

- A running Kubernetes cluster (e.g., `kind`, `minikube`, or a cloud provider).
- `kubectl` configured to point to your cluster.

## Deployment Options

### Option 1: Recommended (Kustomize)

The easiest way to deploy everything at once is using the provided `kustomization.yaml` file. This ensures all resources are applied in a single command.

```bash
kubectl apply -k k8s/
```

### Option 2: Manual Deployment (Order Matters)

If you prefer to apply files individually, follow this order to ensure dependencies (database and storage) are ready before the application starts.

1.  **Namespace**:
    ```bash
    kubectl apply -f k8s/namespace.yaml
    ```
2.  **PostgreSQL**:
    ```bash
    kubectl apply -f k8s/postgres.yaml -n be-minio
    ```
3.  **MinIO**:
    ```bash
    kubectl apply -f k8s/minio.yaml -n be-minio
    ```
4.  **be-minio Application**:
    Wait for PostgreSQL and MinIO pods to be `Running` and `Ready`, then apply the application:
    ```bash
    kubectl apply -f k8s/be-minio.yaml -n be-minio
    ```

## Verification

Check the status of your pods in the `be-minio` namespace:
```bash
kubectl get pods -n be-minio
```

Wait until all pods show `1/1` in the `READY` column.

### Accessing the Application

Since this environment is set up for `kind` with `NodePort` exposure:

- **Application API**: Accessible at `http://localhost:30080` (or `http://<node-ip>:30080`).
- **MinIO Console**: If you want to access the MinIO UI, you may need to use port-forwarding as it is currently exposed via ClusterIP internally:
  ```bash
  kubectl port-forward service/minio 9001:9001 -n be-minio
  ```
  Then visit `http://localhost:9001`.

## Troubleshooting

Check logs for the application:
```bash
kubectl logs -l app=be-minio -f -n be-minio
```

Check logs for PostgreSQL or MinIO:
```bash
kubectl logs -l app=postgres -n be-minio
kubectl logs -l app=minio -n be-minio
```

## Cleanup

To remove all resources created by these manifests:

```bash
kubectl delete -k k8s/
```

In kind NodePort doesn't work, unless you create the cluster with a special config. Workaround:
`kubectl port-forward service/be-minio 8080:8080 -n be-minio`
