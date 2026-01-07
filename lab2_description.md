PRACTICE ASSIGNMENT NO. 2  
Topic: Dockerization and Deployment of an Application from Existing Source Code  

1. General Information  
• Format: Group work (final course project)  
• Duration: 1 week  
• Submission format:  
o Git repository (GitHub or GitLab)  
o PDF report (group report, including individual sections)  
o Screenshot evidence of successful execution  

2. Objectives of the Practice Assignment  
After completing this practice assignment, students will be able to:  
• Understand the role of Docker in Software Engineering and DevOps  
• Know how to:  
o Analyze the source code of a software system  
o Dockerize an application from existing source code  
o Build Docker images and run containers  
o Deploy applications using Docker and Docker Compose  
• Understand fundamental concepts:  
o Docker images and Docker containers  
o Port mapping  
o Environment variables  
o Docker Compose (basic level)  

3. Problem Description  
Each group uses the source code of a course project. The group’s task is to dockerize and successfully deploy the application so that the system can run and be accessed.  

4. Scope and Limitations  
4.1. Allowed Scope  
• Web backend (Node.js, Spring Boot, Python, .NET, PHP, …)  
• Web frontend (React, Vue, Angular, HTML/CSS/JS)  
• Fullstack (Frontend + Backend)  
• Mobile app:  
o Only the backend of the system needs to be dockerized (if any)  

4.2. Not Required  
• Advanced CI/CD  
• Kubernetes  
• Cloud platforms (AWS, GCP, Azure)  
• Domain, HTTPS, monitoring  

5. IMPLEMENTATION CONTENT  

PART A – SOURCE CODE ANALYSIS  
Student groups must analyze the project and present the following in the report:  
1. Application type:  
o Web backend / Web frontend / Fullstack / Mobile (backend)  
2. Technologies used:  
o Programming language  
o Framework  
3. How to run the application without using Docker  
4. Port(s) used by the application  
5. Whether the application uses:  
o A database  
o File upload functionality  

PART B – DOCKERIZING THE APPLICATION  

5.1. Creating a Dockerfile  
• Create a Dockerfile in the root directory of the project  
• The Dockerfile must:  
o Install the appropriate runtime environment  
o Copy the source code  
o Expose the required port(s)  
o Be able to run the application  
o Not hard-code sensitive information (passwords, tokens)  

5.2. Building the Docker Image  
• Build the image from the Dockerfile  
• Clearly specify:  
o The build command  
o The name of the created image  

5.3. Running the Docker Container  
• Run a container from the built image  
• Map ports so the application can be accessed from a browser  
• Verify that the application works correctly  
• Screenshots of the running application are mandatory  

PART C – DEPLOYMENT USING DOCKER COMPOSE  
• Create a docker-compose.yml file  
• Use Docker Compose to deploy:  
o The application  
o (If applicable) the database or supporting services  
• The system must be runnable with a single command: docker-compose up -d  

PART D – REPORT AND INDIVIDUAL CONTRIBUTION ASSESSMENT  

5.4. Group Report  
The group report must present:  
• Source code analysis  
• The dockerization and deployment process  
• Achieved results (with illustrative images)  

5.5. INDIVIDUAL SELF-ASSESSMENT & CONTRIBUTION  
Although the assignment is completed in groups, each student must submit an individual self-assessment to clearly reflect their level of participation and understanding.  

Requirements:  
• Each student submits one individual self-assessment, approximately half an A4 page  
• It may be included in the group report (clearly separated by student)  

The individual self-assessment must include:  
1. The specific tasks personally performed in the group  
(e.g., writing the Dockerfile, configuring docker-compose, building images, deploying the application, testing, writing README, …)  
2. The area of knowledge the student understands best in this practice assignment  
(e.g., Docker images and containers, port mapping, Docker Compose, deployment workflow, …)  
3. One technical difficulty encountered during implementation and how it was resolved  
4. Self-assessment of personal contribution to the group (%)  

The self-assessment must be honest, clear, and contain specific technical content.  
The instructor may use this content to consider adjusting individual scores if necessary.  

6. SUBMITTED DELIVERABLES  

6.1. Git Repository  
Includes:  
• Source code  
• Dockerfile  
• docker-compose.yml  
• README.md with instructions for running the application  

6.2. Report (PDF)  
• Group report  
• Individual assessment and contribution sections for each student  

6.3. Evidence  
• Screenshots of:  
o Built Docker images  
o Running containers  
o Successfully accessed application  

7. EVALUATION CRITERIA  

Content | Weight  
Dockerfile correctness and successful execution | 30%  
Application running successfully |  30%
Docker Compose | 20%   
Report & individual contribution | 20%  

8. NOTES  
• Priority is given to correct understanding of fundamentals; complex configurations are not required  
• A working solution is more important than optimization  
• All group members must understand the entire process, not only their own part  

9. ADVANCED (OPTIONAL – BONUS POINTS)  
• Use of .env files  
• Multi-stage build  
• Pushing images to Docker Hub  
