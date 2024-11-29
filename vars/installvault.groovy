def call() {
    ansiblePlaybook(
                        credentialsId: 'f4fcd509-ca78-47b4-9610-c54171fda58b', 
                        disableHostKeyChecking: true,
                        installation: 'Ansible', 
                        inventory: 'workspace/A6/resources/aws_ec2.yaml',
                        playbook: 'workspace/A6/resources/vault.yaml', 
                        vaultTmpPath: ''
                    )

}
