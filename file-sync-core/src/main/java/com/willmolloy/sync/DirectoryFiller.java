package com.willmolloy.sync;

import java.util.function.Function;

/**
 * Used to fill in missing directories when building the {@link FileTree}.
 *
 * <p>Useful when directories/folders aren't scanned - e.g. AWS S3 ListObjects.
 *
 * @param <DirectoryT> type of directory produced
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@FunctionalInterface
public interface DirectoryFiller<DirectoryT extends File> extends Function<String, DirectoryT> {}
