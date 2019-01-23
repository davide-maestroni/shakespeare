package dm.shakespeare.plot.function;

/**
 * Created by davide-maestroni on 04/26/2018.
 */
public interface SenaryFunction<T1, T2, T3, T4, T5, T6, R> {

  R call(T1 first, T2 second, T3 third, T4 fourth, T5 fifth, T6 sixth) throws Exception;
}
