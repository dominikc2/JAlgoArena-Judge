class WordLadder
  def ladderLength(starts, ends, dict)
    result = 0

    if dict.size == 0
      return result
    end

    dict.add(starts)
    dict.add(ends)

    bfs(starts, ends, dict)
  end

  def bfs(starts, ends, dict)
    queue = java.util.LinkedList.new
    length = java.util.LinkedList.new

    queue.add(starts)
    length.add(1)

    while !queue.empty?
      word = queue.poll
      len = length.poll

      if word == ends
        return len
      end

      word.length.times do |i|
        ('a'..'z').each do |c|
          next if word[i] == c

          word[i] = c

          if dict.contains(word)
            queue.add(word)
            length.add(len + 1)
            dict.remove(word)
          end
        end
      end
    end

    0
  end
end