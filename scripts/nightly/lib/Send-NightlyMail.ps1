# Send-NightlyMail.ps1
# SMTP mail helper for the nightly issue bot. Uses System.Net.Mail so it works
# on both Windows PowerShell 5.1 and PowerShell 7+ (Send-MailMessage is
# deprecated). Credentials come from scripts/nightly/config.local.psd1 — never
# hardcode an app password here.

function Send-NightlyMail {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] $Config,
        [Parameter(Mandatory)] [string] $Subject,
        [Parameter(Mandatory)] [string] $Body
    )

    if (-not $Config.Smtp) { throw "config.local.psd1: missing 'Smtp' block" }
    $smtp = $Config.Smtp
    foreach ($k in 'Host', 'Port', 'User', 'AppPassword', 'From') {
        if ([string]::IsNullOrWhiteSpace([string]$smtp.$k)) {
            throw "config.local.psd1: Smtp.$k is empty — fill it in before enabling the bot"
        }
    }
    $recipients = @($Config.NotifyTo) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if (-not $recipients) { throw "config.local.psd1: 'NotifyTo' is empty" }

    $useSsl = $true
    if ($null -ne $smtp.UseSsl) { $useSsl = [bool]$smtp.UseSsl }

    $mail = [System.Net.Mail.MailMessage]::new()
    try {
        $mail.From = [System.Net.Mail.MailAddress]::new([string]$smtp.From, 'Markleaf nightly bot')
        foreach ($r in $recipients) { $mail.To.Add([string]$r) }
        $mail.Subject = $Subject
        $mail.Body = $Body
        $mail.SubjectEncoding = [System.Text.Encoding]::UTF8
        $mail.BodyEncoding = [System.Text.Encoding]::UTF8

        $client = [System.Net.Mail.SmtpClient]::new([string]$smtp.Host, [int]$smtp.Port)
        try {
            $client.EnableSsl = $useSsl
            $client.DeliveryMethod = [System.Net.Mail.SmtpDeliveryMethod]::Network
            $client.Credentials = [System.Net.NetworkCredential]::new([string]$smtp.User, [string]$smtp.AppPassword)
            $client.Send($mail)
        }
        finally { $client.Dispose() }
    }
    finally { $mail.Dispose() }
}
