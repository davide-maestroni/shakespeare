package dm.shakespeare.plot.function;

/**
 * Created by davide-maestroni on 04/26/2018.
 */
public interface QuaternaryFunction<T1, T2, T3, T4, R> {

  R call(T1 first, T2 second, T3 third, T4 fourth) throws Exception;
}
