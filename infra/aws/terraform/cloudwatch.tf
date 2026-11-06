resource "aws_cloudwatch_log_group" "backend" {
  name              = "/lunacare/${var.environment}/backend"
  retention_in_days = 30

  tags = {
    Name = "lunacare-${var.environment}-backend-logs"
  }
}

resource "aws_sns_topic" "alerts" {
  name = "lunacare-${var.environment}-security-alerts"

  tags = {
    Name = "lunacare-${var.environment}-security-alerts"
  }
}

resource "aws_sns_topic_subscription" "email_alerts" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# Metric Filter for Backend 5xx HTTP Errors
resource "aws_cloudwatch_log_metric_filter" "http_5xx" {
  name           = "lunacare-${var.environment}-5xx-errors"
  pattern        = "{ $.statusCode >= 500 }"
  log_group_name = aws_cloudwatch_log_group.backend.name

  metric_transformation {
    name      = "HTTP5xxCount"
    namespace = "LunaCare/Backend"
    value     = "1"
  }
}

# Alarm when 5xx errors exceed threshold (5 in 5 minutes)
resource "aws_cloudwatch_metric_alarm" "backend_5xx" {
  alarm_name          = "lunacare-${var.environment}-high-5xx-rate"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "HTTP5xxCount"
  namespace           = "LunaCare/Backend"
  period              = 300
  statistic           = "Sum"
  threshold           = 5
  alarm_description   = "Triggered when LunaCare backend returns more than 5 server errors within 5 minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
