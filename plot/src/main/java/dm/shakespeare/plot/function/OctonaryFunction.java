package dm.shakespeare.plot.function;

/**
 * Created by davide-maestroni on 04/26/2018.
 */
public interface OctonaryFunction<T1, T2, T3, T4, T5, T6, T7, T8, R> {

  R call(T1 first, T2 second, T3 third, T4 fourth, T5 fifth, T6 sixth, T7 seventh, T8 eighth) throws
      Exception;
}
