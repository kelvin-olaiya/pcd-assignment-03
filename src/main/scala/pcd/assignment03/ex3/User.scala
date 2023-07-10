package pcd.assignment03.ex3

import pcd.assignment03.ex2.pixelart.Brush

import java.util.UUID
import scala.util.Random

trait User:
  def userId: UUID
  def brush: Brush

object User:
  def apply(): User = UserImpl()
  private case class UserImpl() extends User:
    override val userId: UUID = UUID.randomUUID()
    override val brush: Brush = Brush(0, 0, Random.nextInt(256 * 256 * 256))

