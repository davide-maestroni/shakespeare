package dm.shakespeare.plot.function;

/**
 * Created by davide-maestroni on 04/26/2018.
 */
public interface BinaryFunction<T1, T2, R> {

  R call(T1 first, T2 second) throws Exception;
}
