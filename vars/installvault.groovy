def call() {
    ansiblePlaybook(
                        credentialsId: '1dfb88aa-b117-42bf-a70a-12e98a81d7d1',
                        disableHostKeyChecking: true,
                        installation: 'ansible',
                        inventory: '/resources/aws_ec2.yaml',
                        playbook: '/resources/vault.yaml',
                        vaultTmpPath: ''
                    )

}
