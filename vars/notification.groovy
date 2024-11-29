def call(String status, String channel) {
    if (status == 'SUCCESS') {
        slackSend channel: 'visca-barca', message: 'Success'
    } else {
        slackSend channel: 'visca-barca', message: 'Fail'
    }
}
