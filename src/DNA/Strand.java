package DNA;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Strand {
	public String strand_string;

	public Strand(String strand){
		this.strand_string = strand;
	}
	
	@Override
	public boolean equals(Object o){
		if (!(o instanceof Strand)){
			return false;
		}
		Strand new_strand = (Strand) o;
		return (this.strand_string.equals(new_strand.strand_string));
	}
	

    @Override
	public int hashCode(){
		return strand_string.hashCode();
	}


	public int similarity(Strand s){
        int count = 0;
        for(int i = 0; i < strand_string.length(); i++)
        {
            if (strand_string.charAt(i) == s.strand_string.charAt(i))
                count++;
        }

		return count;
	}

    /* Returns strand with most common base at each index */
	public static Strand getCentroid(List<Strand> strands)
    {
        int strand_length = strands.get(0).strand_string.length();
        int[][] bases = new int[strand_length][4];
        //Stores counts for each index: A:0 G:1 C:2 T:3
		for (Strand s : strands){
            for(int i = 0; i < strand_length; i++)
            {
                switch (s.strand_string.charAt(i))
                {
                    case 'A':
                        bases[i][0] =+ 1;
                        break;
                    case 'G':
                        bases[i][1] =+ 1;
                        break;
                    case 'C':
                        bases[i][2] =+ 1;
                        break;
                    case 'T':
                        bases[i][3] =+ 1;
                        break;
                }
            }
		}

        char[] centroidChars = new char[strand_length];

        // Store most frequent character for each index
        for(int i = 0; i < strand_length; i++)
        {
            int maxIndex = 0;
            int maxVal = 0;
            for (int j = 0; j < 4; j++)
            {
                if(bases[i][j] > maxVal)
                {
                    maxVal = bases[i][j];
                    maxIndex = j;
                }

            }

            switch (maxIndex)
            {
                case 0:
                    centroidChars[i] = 'A';
                    break;
                case 1:
                    centroidChars[i] = 'G';
                    break;
                case 2:
                    centroidChars[i] = 'C';
                    break;
                case 3:
                    centroidChars[i] = 'T';
                    break;
            }
        }
		return new Strand(new String(centroidChars));
	}

	
	public int closetPointIndex(Strand[] centroids) {
		int temp_similarity;
		int max_similarity = 0;
		int close_index = 0;
		Strand p;
		for (int i = 0; i < centroids.length; i++){
			p = centroids[i];
			temp_similarity = this.similarity(p);
			if (temp_similarity > max_similarity){
				max_similarity = temp_similarity;
				close_index = i;
			}
		}
		return close_index;
	}
	
}
