import HelloWorld.Messages.Greet
import HelloWorldMain.SayHello
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object HelloWorld:
    enum Messages:
        case Greet(whom: String, replyTo: ActorRef[Greeted])
        case Greeted(whom: String, from: ActorRef[Greet])

    import Messages.*

    def apply(): Behavior[Greet] = Behaviors.receive { (context, message) =>
        context.log.info("Hello {}!", message.whom)
        message.replyTo ! Greeted(message.whom, context.self)
        Behaviors.same
    }

object HelloWorldBot:
    import HelloWorld.Messages.*

    def apply(maxGreetings: Int): Behavior[Greeted] = bot(0, maxGreetings)

    private def bot(greetingsCount: Int, max: Int): Behavior[Greeted] =
        Behaviors.receive { (context, message) =>
            if (greetingsCount < max) {
                context.log.info(s"Greeting ${greetingsCount+1} for ${message.whom}")
                message.from ! Greet(message.whom, context.self)
                bot(greetingsCount + 1, max)
            } else {
                Behaviors.stopped
            }
        }

object HelloWorldMain:
    import HelloWorld.Messages.Greet
    case class SayHello(name: String)

    def apply(): Behavior[SayHello] = Behaviors.setup { context =>
        val greeter = context.spawn(HelloWorld(), "greeter")
        Behaviors.receiveMessage { message =>
            val greetBot = context.spawn(HelloWorldBot(maxGreetings = 3), message.name)
            greeter ! Greet(message.name, replyTo = greetBot)
            Behaviors.same
        }
    }

object Main extends App:
    import HelloWorldMain.SayHello
    val system = ActorSystem(guardianBehavior = HelloWorldMain(), name = "hello")

    system ! SayHello("Kel")
    system ! SayHello("Andru")