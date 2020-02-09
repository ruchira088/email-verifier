
resource "aws_s3_bucket_object" "failure_image" {
  source = "../../assets/broken-robot.png"
  bucket = var.s3_bucket
  key = "email/broken-robot.png"
  content_type = "image/png"
  acl = "public-read"

  etag = filemd5("../../assets/broken-robot.png")
}

resource "aws_s3_bucket_object" "verification_image" {
  source = "../../assets/happy-minions.jpg"
  bucket = var.s3_bucket
  key = "email/happy-minions.jpg"
  content_type = "image/jpeg"
  acl = "public-read"

  etag = filemd5("../../assets/happy-minions.jpg")
}