
terraform {
  backend "s3" {}
}

provider "aws" {
  version = "~> 2.0"
  region = "ap-southeast-2"
}

variable "s3_bucket" {
  type = string
}