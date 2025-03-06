#  OT- MS Application Architecture

## Overview

This document provides a detailed overview of the architecture for the OTMS (Online Tracking Management System) application deployed on AWS. It outlines the various components, their interactions, network configurations, and security considerations.

## Architecture Diagram

![image](https://github.com/user-attachments/assets/fc133d48-5660-4224-8d2b-3d0d1d4d9266)


## Workflow

1.  **User Interaction:** Users access the OTMS application via the internet.
2.  **DNS Resolution:** Route53 resolves the application's domain name to the Elastic Load Balancer's (ALB) address.
3.  **Load Balancing:** The ALB distributes incoming traffic across available instances in the frontend subnet.
4.  **Frontend Processing:** The frontend processes user requests and interacts with the backend application servers (Salary, Attendance, Employee APIs).  The frontend runs on port 3000.
5.  **Application Logic:** The application servers handle business logic, interacting with the database layer (Redis for caching, ScyllaDB, and PostgreSQL).
6.  **Data Storage:** The database layer stores and retrieves data as required by the application.

## Detailed Component Description

### 1. AWS Cloud (us-east-1)

*   **Region:** The application is deployed in the `us-east-1` AWS region.
*   **VPC (otms\_vpc - 192.168.0.0/24):** A Virtual Private Cloud (VPC) provides a logically isolated network environment. The CIDR block `192.168.0.0/24` defines the IP address range for the VPC.

### 2. Subnets

*   **Public Subnet (192.168.0.0/28):** This subnet hosts resources that need to be directly accessible from the internet.
    *   **OpenVPN:** An OpenVPN server resides in the public subnet, likely for secure administrative access or user VPN connectivity.
    *   **Security Group:** A security group associated with the OpenVPN instance controls inbound and outbound traffic.
    *   **NAT Gateway:** The NAT Gateway allows instances in the private subnets to access the internet for updates and external services without being directly exposed.
*   **Frontend Subnet (192.168.0.16/28):** This subnet hosts the frontend application servers.
    *   **Frontend Instances:** Instances running the frontend application code. Frontend runs on port 3000.
    *   **Security Group:** Controls inbound traffic to the frontend instances (likely from the ALB) and outbound traffic to the application subnet.
*   **Application Subnet (192.168.0.32/28):** Contains the application servers responsible for core business logic.
    *   **Application Instances:** Instances running the backend application code:
        *   Salary API (port 8080)
        *   Attendance API (port 8081)
        *   Employee API (port 8082)
    *   **Security Group:** Controls inbound traffic from the frontend and outbound traffic to the database subnet.
*   **Database Subnet (192.168.0.48/28):** Hosts the database instances.
    *   **Database Instances:** Instances of `redis`, `scylla`, and `postgresql`.
    *   **Security Group:** Restricts inbound traffic to only the application servers and outbound traffic as required.

### 3. Network Components

*   **Internet Gateway (IGW):** Enables communication between the VPC and the internet.
*   **Route53:** AWS's DNS service, used to route traffic to the ALB.
*   **ALB (otms\_alb):** Elastic Load Balancer distributes incoming traffic across the frontend instances.
    *   Listens on ports 80 and 443.
    *   **Security Group:** Controls inbound traffic from the internet (HTTP/HTTPS) and outbound traffic to the frontend instances.
*   **NAT Gateway:** Allows instances in the private subnets to initiate outbound traffic to the Internet, but prevents the Internet from initiating a connection with those instances.
*   **Route Tables:** Control the routing of traffic within the VPC.
    *   **Public Route Table (public\_rt):** Routes traffic to the IGW.
    *   **Private Route Table (private\_rt):** Routes traffic to the NAT Gateway and potentially internally within the VPC.

## Security

*   **Security Groups:** Security Groups act as virtual firewalls, controlling inbound and outbound traffic at the instance level. Each subnet has an associated security group.
*   **Principle of Least Privilege:** Security groups should be configured to allow only the necessary traffic, following the principle of least privilege.
*   **OpenVPN:** Provides secure remote access to the VPC.
*   **Network ACLs (NACLs):** Network ACLs provide an optional layer of security that acts as a firewall for controlling traffic in and out of subnets.
*   **Regular Security Audits:** Conduct regular security audits to identify and address potential vulnerabilities.

## Ports and Protocols

### Security Groups Rules

| Security Group     | Direction | Source/Destination | Port(s)                                  | Protocol | Description                                                                 |
| ------------------ | --------- | ------------------ | ---------------------------------------- | -------- | --------------------------------------------------------------------------- |
| **ALB**            | Inbound   | 0.0.0.0/0          | 80, 443                                  | TCP      | HTTP/HTTPS traffic from the internet                                        |
| **ALB**            | Outbound  | Frontend SG        | 3000                                  | TCP      | Traffic to Frontend Instances on port 3000                                           |
| **Frontend SG**   | Inbound   | ALB SG             | 3000                                   | TCP      | Traffic from ALB                                                              |
| **Frontend SG**   | Outbound  | Application SG     | 8080, 8081, 8082, 6379, Ephemeral Ports | TCP      | Traffic to Salary, Attendance, Employee APIs and Redis                              |
| **Application SG** | Inbound   | Frontend SG        | 8080, 8081, 8082                         | TCP      | Traffic from Frontend                                                           |
| **Application SG** | Outbound  | Database SG        | 6379, 9042, 5432, Ephemeral Ports         | TCP      | Traffic to Redis, ScyllaDB, and PostgreSQL. Ephemeral ports for responses.  |
| **Database SG**    | Inbound   | Application SG     | 6379, 9042, 5432                         | TCP      | Traffic from Application Instances to Redis (6379), ScyllaDB (9042), PostgreSQL (5432) |
| **Database SG**    | Outbound  | Application SG     | Ephemeral Ports                          | TCP      | Responses to Application Instances                                            |
| **OpenVPN SG**     | Inbound   | 0.0.0.0/0          | 1194                                   | UDP      | OpenVPN traffic                                                                 |
| **OpenVPN SG**     | Outbound  | 0.0.0.0/0          | 1194                                   | UDP      | OpenVPN traffic                                                                 |

*   **Ephemeral Ports:** Ephemeral ports are temporary ports assigned by the operating system for outbound connections. The specific range varies by OS. Typically 1024-65535 but this can vary.

### Network ACL (NACL) Rules (Example)

These are example rules. Adjust based on your specific needs.  NACLs are stateless, so you need to define rules for both inbound and outbound traffic.

**Public Subnet NACL**

| Rule # | Direction | Source/Destination | Port(s)      | Protocol | Allow/Deny | Description                                       |
| ------ | --------- | ------------------ | ------------ | -------- | ---------- | ------------------------------------------------- |
| 100    | Inbound   | 0.0.0.0/0          | 80, 443, 1194| TCP/UDP  | ALLOW      | Allow HTTP, HTTPS, and OpenVPN traffic from Internet |
| 110    | Outbound  | 0.0.0.0/0          | Ephemeral Ports| TCP      | ALLOW      | Allow outbound responses                              |
| 120    | Outbound  | 0.0.0.0/0          | 1194         | UDP      | ALLOW      | Allow outbound OpenVPN traffic                      |
| 99     | Inbound   | 0.0.0.0/0          | ALL          | ALL      | DENY       | Deny all other inbound traffic                     |
| 100    | Outbound   | 0.0.0.0/0          | ALL          | ALL      | DENY       | Deny all other outbound traffic                     |

**Frontend Subnet NACL**

| Rule # | Direction | Source/Destination | Port(s)                   | Protocol | Allow/Deny | Description                                                                    |
| ------ | --------- | ------------------ | ------------------------- | -------- | ---------- | ------------------------------------------------------------------------------ |
| 100    | Inbound   | 192.168.0.0/24     | 3000                      | TCP      | ALLOW      | Allow traffic from ALB (assuming ALB's IP is in VPC CIDR)                      |
| 110    | Outbound  | 192.168.0.32/28    | 8080, 8081, 8082, 6379, Ephemeral Ports | TCP      | Allow traffic to Application Subnet (Salary, Attendance, Employee APIs and Redis)    |
| 99     | Inbound   | 0.0.0.0/0          | ALL                       | ALL      | DENY       | Deny all other inbound traffic                                                 |
| 100    | Outbound  | 0.0.0.0/0          | ALL                       | ALL      | DENY       | Deny all other outbound traffic                                                |

**Application Subnet NACL**

| Rule # | Direction | Source/Destination | Port(s)             | Protocol | Allow/Deny | Description                                                                         |
| ------ | --------- | ------------------ | ------------------- | -------- | ---------- | ----------------------------------------------------------------------------------- |
| 100    | Inbound   | 192.168.0.16/28    | 8080, 8081, 8082    | TCP      | ALLOW      | Allow traffic from Frontend Subnet (Salary, Attendance, Employee APIs)             |
| 110    | Outbound  | 192.168.0.48/28    | 6379, 9042, 5432, Ephemeral Ports | TCP      | Allow traffic to Database Subnet (Redis, ScyllaDB, PostgreSQL). Ephemeral for return.|
| 99     | Inbound   | 0.0.0.0/0          | ALL                 | ALL      | DENY       | Deny all other inbound traffic                                                      |
| 100    | Outbound  | 0.0.0.0/0          | ALL                 | ALL      | DENY       | Deny all other outbound traffic                                                     |

**Database Subnet NACL**

| Rule # | Direction | Source/Destination | Port(s)             | Protocol | Allow/Deny | Description                                          |
| ------ | --------- | ------------------ | ------------------- | -------- | ---------- | ---------------------------------------------------- |
| 100    | Inbound   | 192.168.0.32/28    | 6379, 9042, 5432    | TCP      | ALLOW      | Allow traffic from Application Subnet (Redis, ScyllaDB, PostgreSQL) |
| 110    | Outbound  | 192.168.0.32/28    | Ephemeral Ports     | TCP      | ALLOW      | Allow return traffic to Application Subnet          |
| 99     | Inbound   | 0.0.0.0/0          | ALL                 | ALL      | DENY       | Deny all other inbound traffic                      |
| 100    | Outbound  | 0.0.0.0/0          | ALL                 | ALL      | DENY       | Deny all other outbound traffic                     |

**Important Notes about NACLs:**

*   NACLs are stateless; rules apply to both inbound and outbound traffic.  This is why you see rules allowing ephemeral ports in both directions.
*   NACLs are evaluated in order, starting with the lowest rule number.  Once a rule matches, it's applied.
*   The last rule in a NACL should always be a `DENY ALL` rule to explicitly deny any traffic that doesn't match any of the previous rules.
*   The source/destination in NACL rules refer to CIDR blocks, i.e., IP address ranges.
*   These are example rules. Adapt them to your specific security requirements.  It is crucial to assess the risks and adjust the rules accordingly.
*   Be very careful when configuring NACLs; incorrect rules can easily lock you out of your instances.  Test thoroughly!

## Data Flow

1.  User initiates a request via the internet.
2.  Route53 resolves the domain to the ALB.
3.  ALB forwards the request to a healthy frontend instance on port 3000.
4.  Frontend processes the request and sends queries to the application servers (Salary on 8080, Attendance on 8081, Employee on 8082).
5.  Application servers perform business logic and interact with the appropriate database:
    *   Redis is used for caching.
    *   Salary and Employee APIs interact with ScyllaDB.
    *   Attendance API interacts with PostgreSQL.
6.  Data is retrieved from/stored in the database.
7.  Application server sends the response back to the frontend.
8.  Frontend sends the response back to the user via the ALB.

## Considerations and Improvements

*   **Scalability:** Implement autoscaling groups for the frontend and application tiers to handle varying loads.
*   **Monitoring:** Implement comprehensive monitoring using CloudWatch to track application performance and infrastructure health.
*   **Disaster Recovery:** Implement a disaster recovery plan, including backups and replication to another region.
*   **Infrastructure as Code (IaC):** Manage infrastructure using tools like Terraform or CloudFormation for consistency and repeatability.
*   **Centralized Logging:** Implement centralized logging using services like CloudWatch Logs or Elasticsearch.
*   **NACL Refinement:**  Carefully refine NACL rules based on actual traffic patterns and security requirements.

This updated `README.md` includes detailed port information, security group rules, example NACL rules, and considers ephemeral ports.  Remember to replace the architecture diagram placeholder with the actual diagrams.
