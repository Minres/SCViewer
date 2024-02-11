import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;

public class SCViewerMatchingStrategy implements IEditorMatchingStrategy {

	@Override
	public boolean matches(IEditorReference editorRef, IEditorInput input) {
		return input.getName().endsWith(".ftr") || input.getName().endsWith(".fst") 
				|| input.getName().endsWith(".vcd") || input.getName().endsWith(".txlog")
				|| input.getName().endsWith(".txlog.gz") || input.getName().endsWith(".txdb");
	}

}
