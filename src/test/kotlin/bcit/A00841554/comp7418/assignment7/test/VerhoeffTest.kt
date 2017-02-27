package bcit.A00841554.comp7418.assignment7.test

import bcit.A00841554.comp7418.assignment7.Verhoeff
import com.github.ericytsang.lib.lazycollections.lazyMapNotNull
import com.github.ericytsang.lib.randomstream.RandomInputStream
import org.junit.Test
import java.io.DataInputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class VerhoeffTest
{
    fun swap(i1:Int,i2:Int,string:String):String
    {
        val iMin = Math.min(i1,i2)
        val iMax = Math.max(i1,i2)
        val stb = StringBuilder()
        stb.append(string,0,iMin)
        stb.append(string[iMax])
        stb.append(string,iMin+1,iMax)
        stb.append(string[iMin])
        stb.append(string,iMax+1,string.length)
        return stb.toString()
    }

    @Test
    fun swapTest()
    {
        assert(swap(0,1,"abcde") == "bacde")
        assert(swap(2,1,"abcde") == "acbde")
        assert(swap(2,3,"abcde") == "abdce")
        assert(swap(4,3,"abcde") == "abced")
    }

    fun adjacentTranspositions(digits:String):Iterable<String>
    {
        return (0..digits.lastIndex-1).lazyMapNotNull {
            if (digits[it] == digits[it+1])
                null
            else
                swap(it,it+1,digits)
        }
    }

    @Test
    fun adjacentTranspositionsTest()
    {
        assert(adjacentTranspositions("abcde").toSet() == setOf("bacde","acbde","abdce","abced"))
    }

    fun findCollisionPreimages(digits:String,transform:(String)->String):List<String>
    {
        val originalTransformed = transform(digits)
        return adjacentTranspositions(digits)
            .filter {transform(it) == originalTransformed}
    }

    fun findVerhoeffCollisionPreimages(digits:String):List<String>
    {
        return findCollisionPreimages(digits,{Verhoeff.generateVerhoeff(it)})
    }

    fun generateNumericString(length:Int):String
    {
        val result = StringBuilder()
        val randomI = DataInputStream(RandomInputStream())
        while (result.length < length)
        {
            result.append(Math.abs(randomI.readLong()))
        }
        return result.toString().substring(0,length)
    }

    @Test
    fun verhoeffStrengthTest()
    {
        var length = 1
        var step = 1
        val mutex = ReentrantLock()
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        var task:()->Unit = {}
        task = {
            executor.execute(task)
            val _length = mutex.withLock {
                length = try
                {
                    Math.addExact(length,step)
                }
                catch (ex:ArithmeticException)
                {
                    executor.shutdown()
                    Int.MAX_VALUE
                }
                step *= 2
                length
            }
            println("started computing for number string of length $_length")
            val numbers = generateNumericString(_length)
            val equivalents = findVerhoeffCollisionPreimages(numbers)
            println("finished computing for number string of length $_length")
            if (equivalents.isNotEmpty())
            {
                mutex.withLock {
                    length  = _length
                    step = 1
                }
                println("${equivalents.size}:$_length:$numbers:$equivalents")
            }
        }
        executor.execute(task)
        executor.awaitTermination(Long.MAX_VALUE,TimeUnit.DAYS)
    }
}
