package com.logicalclocks.commons

import java.util.Properties

import org.scalatest.FunSuite
import org.scalatest.Matchers

class PropertiesReaderSuite extends FunSuite with Matchers {
  test("new PropertiesReader should have empty properties") {
    val pr: Properties = PropertiesReader().props

    pr.isEmpty shouldBe true
  }

  test("PropertiesReader should correctly read one resource file") {
    val pr: Properties = PropertiesReader()
      .addResource("test-properties-1.conf", "test1")
      .props

    pr.size() shouldBe 3
    pr.get("name").toString shouldBe "properties reader test"
    pr.get("number").toString.toInt shouldBe 1
    pr.get("group.id").toString shouldBe "tests"
  }

  test("PropertiesReader should correctly read two resource files") {
    val pr: Properties = PropertiesReader()
      .addResource("test-properties-1.conf", "test1")
      .addResource("test-properties-2.conf", "test2")
      .props

    pr.size shouldBe 5
    pr.get("name").toString shouldBe "properties reader test"
    pr.get("number").toString.toInt shouldBe 2
    pr.get("group.id").toString shouldBe "tests"
    pr.get("age").toString.toInt shouldBe 20
    pr.get("email.address").toString shouldBe "name@mail.com"

  }
}
