package dm.shakespeare.plot.function;

/**
 * Created by davide-maestroni on 04/26/2018.
 */
public interface UnaryFunction<T1, R> {

  R call(T1 first) throws Exception;
}
