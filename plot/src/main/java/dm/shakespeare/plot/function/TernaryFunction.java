package dm.shakespeare.plot.function;

/**
 * Created by davide-maestroni on 04/26/2018.
 */
public interface TernaryFunction<T1, T2, T3, R> {

  R call(T1 first, T2 second, T3 third) throws Exception;
}
