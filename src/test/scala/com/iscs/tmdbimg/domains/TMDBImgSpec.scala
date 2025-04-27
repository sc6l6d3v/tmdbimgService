package com.iscs.tmdbimg.domains

import zio._
import zio.json._
import zio.test.Assertion._
import zio.test.{TestAspect, _}

object TMDBImgSpec extends ZIOSpecDefault {
  private val kid1 =
    """
      |{"Child1":{"prop1":15}}
      |""".stripMargin

  private val kid1Hint = """{"hint":"child1","prop1":15}"""

  private val kid2 =
    """
      |{"Child2":{"prop2":"25"}}
      |""".stripMargin

  private val kid2Hint = """{"hint":"child2","prop2":"25"}"""

  private val emptyKids =
    """
      |{
      |  "kid1":[],
      |  "kid2":[]
      |}
      |""".stripMargin

  private val testKidsLeft =
    """
      |{
      |  "kid1":[{"Child1":{
      |          "prop1":15
      |        }
      |    }],
      |  "kid2":[]
      |}
      |""".stripMargin

  private val testKidsRight =
    """
      |{
      |  "kid1":[],
      |  "kid2":[{"Child2":{
      |          "prop2":"25"
      |        }
      |  }]
      |}
      |""".stripMargin

  private val testKidsLeftHint =
    """
      |{
      |  "kid1":[{"hint":"child1",
      |        "prop1":15
      |    }],
      |  "kid2":[]
      |}
      |""".stripMargin

  private val testKidsRightHint =
    """
      |{
      |  "kid1":[],
      |  "kid2":[{"hint":"child2",
      |        "prop2":"25"
      |  }]
      |}
      |""".stripMargin

  val spec: Spec[Environment, Any] =
    suite("TMDBImgSpec")(
      test("check kid1") {
        import examplenohintsum._

        assert(kid1.fromJson[Parent])(isRight(equalTo(Child1(15))))
      },
      test("check kid2") {
        import examplenohintsum._

        assert(kid2.fromJson[Parent])(isRight(equalTo(Child2("25"))))
      },
      test("check kid1 hint") {
        import examplehintsum._

        assert(kid1Hint.fromJson[Parent])(isRight(equalTo(Child1(15))))
      },
      test("check kid2 hint") {
        import examplehintsum._

        assert(kid2Hint.fromJson[Parent])(isRight(equalTo(Child2("25"))))
      },
      test("check empty kids") {
        import examplehintsum._

        val emptyKidsCC = Kids(List.empty[Parent], List.empty[Parent])

        assert(emptyKids.fromJson[Kids])(isRight(equalTo(emptyKidsCC)))
      },
      test("check left kid") {
        import examplenohintsum._

        val child1: Parent = Child1(15)
        val kidsCC         = Kids(List(child1), List.empty[Parent])

        assert(testKidsLeft.fromJson[Kids])(isRight(equalTo(kidsCC)))
      },
      test("check right kid") {
        import examplenohintsum._

        val child2: Parent = Child2("25")
        val kidsCC         = Kids(List.empty[Parent], List(child2))

        assert(testKidsRight.fromJson[Kids])(isRight(equalTo(kidsCC)))
      },
      test("check left kid hint") {
        import examplehintsum._

        val child1: Parent = Child1(15)
        val kidsCC         = Kids(List(child1), List.empty[Parent])

        assert(testKidsLeftHint.fromJson[Kids])(isRight(equalTo(kidsCC)))
      },
      test("check right kid hint") {
        import examplehintsum._

        val child2: Parent = Child2("25")
        val kidsCC         = Kids(List.empty[Parent], List(child2))

        assert(testKidsRightHint.fromJson[Kids])(isRight(equalTo(kidsCC)))
      }
    )

  object examplehintsum {
    @jsonDiscriminator("hint")
    sealed abstract class Parent

    object Parent {
      implicit val decoder: JsonDecoder[Parent] = DeriveJsonDecoder.gen[Parent]
    }

    @jsonHint("child1")
    case class Child1(prop1: Int) extends Parent

    @jsonHint("child2")
    case class Child2(prop2: String) extends Parent

    @jsonHint("kids")
    case class Kids(kid1: List[Parent], kid2: List[Parent])

    object Kids {
      implicit val decoder: JsonDecoder[Kids] = DeriveJsonDecoder.gen[Kids]
    }
  }

  object examplenohintsum {
    sealed abstract class Parent

    object Parent {
      implicit val decoder: JsonDecoder[Parent] = DeriveJsonDecoder.gen[Parent]
    }

    case class Child1(prop1: Int) extends Parent

    case class Child2(prop2: String) extends Parent

    case class Kids(kid1: List[Parent], kid2: List[Parent])

    object Kids {
      implicit val decoder: JsonDecoder[Kids] = DeriveJsonDecoder.gen[Kids]
    }
  }
}
