# cr-groovy


. Set Up Your GCP Environment

Enable necessary APIs (Compute Engine, Kubernetes Engine).

Create a GCP project and service account with IAM roles (e.g., roles/container.admin, roles/monitoring.admin).



---

2. Create a GKE Cluster with Terraform

Use Terraform's google_container_cluster resource to create the GKE cluster.

Include node pool definitions using google_container_node_pool.



---

3. Deploy Prometheus via Helm in Terraform

Use the Terraform Helm provider to deploy Prometheus.

Add the Helm chart:

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

resource "helm_release" "prometheus" {
  name       = "prometheus"
  chart      = "prometheus"
  repository = "https://prometheus-community.github.io/helm-charts"
  namespace  = "monitoring"
  create_namespace = true
}



---

4. Configure Prometheus Access

Expose Prometheus via LoadBalancer, NodePort, or Ingress depending on your needs.

Optionally configure authentication (basic auth, OAuth, etc.).



---

5. (Optional) Connect to Grafana

You can deploy Grafana using Terraform + Helm as well, and configure it to use Prometheus as a data source.